class CrossfadePlayer
{
    /**
     * @param {number} crossfadeDuration - Время наложения треков в миллисекундах (например, 3000мс = 3 сек)
     */
    constructor(crossfadeDuration = 3000)
    {
        this.duration = crossfadeDuration;
        this.volume = 1.0; // Общая громкость плеера

        // Создаем два параллельных аудио-движка
        this.channelA = new Audio();
        this.channelB = new Audio();

        // Активный канал, который играет прямо сейчас
        this.activeChannel = this.channelA;
        this.fadeIntervals = [];
    }

    // Возвращает текущий играющий элемент для навешивания событий (timeupdate и т.д.)
    get audio()
    {
        return this.activeChannel;
    }

    setVolume(value)
    {
        this.volume = Number(value);
        if (this.fadeIntervals.length === 0)
        {
            this.activeChannel.volume = this.volume;
        }
    }

    // Плавный старт (Fade In)
    play() {
        this._clearFadeAnimations();
        if (!this.activeChannel.src)
            return;

        if (this.activeChannel.paused)
        {
            this.activeChannel.volume = 0;
            this.activeChannel.play().catch(e => console.log("Crossfade play block:", e));
        }

        this._animateVolume(this.activeChannel, 0, this.volume, this.duration);
    }

    // Плавный стоп (Fade Out)
    pause()
    {
        this._clearFadeAnimations();
        if (this.activeChannel.paused)
            return;

        this._animateVolume(this.activeChannel, this.activeChannel.volume, 0, this.duration, () =>
        {
            this.activeChannel.pause();
        });
    }

    /**
     * Запуск нового трека с наложением на старый
     * @param {string} nextTrackUrl - Ссылка на следующий трек
     */
    crossfadeTo(nextTrackUrl)
    {
        this._clearFadeAnimations();

        const oldChannel = this.activeChannel;
        const newChannel = (oldChannel === this.channelA) ? this.channelB : this.channelA;

        newChannel.src = nextTrackUrl;
        newChannel.volume = 0;

        this.activeChannel = newChannel;

        newChannel.play()
            .then(() =>
            {
                this._animateVolume(newChannel, 0, this.volume, this.duration);

                if (!oldChannel.paused)
                {
                    this._animateVolume(oldChannel, oldChannel.volume, 0, this.duration, () =>
                     {
                        oldChannel.pause();
                        oldChannel.src = "";
                    });
                }
            })
            .catch(err =>
            {
                console.error("Ошибка кроссфейда при старте нового трека:", err);
                this.activeChannel = oldChannel;
            });
    }

    _animateVolume(channel, startVol, endVol, duration, callback)
    {
        const steps = 30;
        const stepTime = duration / steps;
        const volDelta = (endVol - startVol) / steps;
        let currentStep = 0;

        channel.volume = startVol;

        const interval = setInterval(() =>
        {
            currentStep++;
            let nextVol = startVol + (volDelta * currentStep);

            nextVol = Math.max(0, Math.min(1, nextVol));
            channel.volume = nextVol;

            if (currentStep >= steps)
            {
                channel.volume = endVol;
                clearInterval(interval);
                this.fadeIntervals = this.fadeIntervals.filter(i => i !== interval);
                if (callback)
                    callback();
            }
        }, stepTime);

        this.fadeIntervals.push(interval);
    }

    _clearFadeAnimations()
    {
        this.fadeIntervals.forEach(clearInterval);
        this.fadeIntervals = [];
    }
}