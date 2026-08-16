class AudioFader
{
    /**
     * @param {HTMLAudioElement} audio - Ссылка на аудио объект
     * @param {number} duration - Длительность анимации в миллисекундах (по умолчанию 300мс)
     */
    constructor(audio, duration = 300)
    {
        this.audio = audio;
        this.duration = duration;
        this.intervalId = null;
        this.targetVolume = 1.0;
    }

    /**
     * Запоминает базовую громкость
     * @param {number} vol - Значение от 0.0 до 1.0
     */
    setTargetVolume(vol)
     {
        this.targetVolume = Number(vol);
        if (!this.intervalId)
        {
            this.audio.volume = this.targetVolume;
        }
    }

    /**
     * Плавно запускает музыку с нарастанием звука
     */
    fadeIn()
    {
        clearInterval(this.intervalId);

        if (this.audio.paused)
        {
            this.audio.volume = 0;
            this.audio.play().catch(err => console.log("FadeIn play block:", err));
        }

        const steps = 20;
        const stepTime = this.duration / steps;
        const volumeStep = this.targetVolume / steps;

        this.intervalId = setInterval(() =>
        {
            if (this.audio.volume + volumeStep >= this.targetVolume)
             {
                this.audio.volume = this.targetVolume;
                clearInterval(this.intervalId);
                this.intervalId = null;
            } else
            {
                this.audio.volume += volumeStep;
            }
        }, stepTime);
    }

    /**
     * Плавно глушит музыку и затем ставит на паузу
     */
    fadeOut()
    {
        clearInterval(this.intervalId);

        if (this.audio.paused) return;

        const steps = 20;
        const stepTime = this.duration / steps;
        const volumeStep = this.audio.volume / steps;

        this.intervalId = setInterval(() =>
        {
            if (this.audio.volume - volumeStep <= 0.005)
            {
                this.audio.volume = 0;
                this.audio.pause();
                clearInterval(this.intervalId);
                this.intervalId = null;
            } else {
                this.audio.volume -= volumeStep;
            }
        }, stepTime);
    }
}
