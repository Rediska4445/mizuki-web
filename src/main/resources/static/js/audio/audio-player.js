export class AudioPlayer
{
    constructor()
    {
        this.audio = new Audio();
        this.isPlaying = false;
        this.isPaused = false;
        this.duration = 0;

        this.handlers =
        {
            onPlay: [],
            onPause: [],
            onStop: [],
            onTimeUpdate: [],
            onEnded: [],
            onPcmProcess: null
        };

        this._initAudioEvents();
    }

    async load(url)
    {
        this.stop();
        this.audio.onloadedmetadata = null;
        this.audio.src = url;
        this.audio.load();

        return new Promise((resolve) =>
        {
            this.audio.onloadedmetadata = () =>
            {
                this.duration = this.audio.duration;
                resolve();
            };
        });
    }

    play()
    {
        if (!this.audio.src)
            return;

        this.audio.play()
            .then(() =>
            {
                this.isPlaying = true;
                this.isPaused = false;
                this._trigger('onPlay');
            })
            .catch(e => console.error("error load audio:", e));
    }

    pause()
    {
        if (this.audio.paused)
            return;

        this.audio.pause();
        this.isPlaying = false;
        this.isPaused = true;
        this._trigger('onPause');
    }

    stop()
    {
        this.audio.pause();
        this.audio.currentTime = 0;
        this.isPlaying = false;
        this.isPaused = false;

        this._trigger('onStop');
        this._trigger('onTimeUpdate', 0);
    }

// TODO: Make the error on if condition
    seek(seconds)
    {
        if (!this.audio.src)
            return;

        let target = seconds;
        if (target < 0)
            target = 0;
        if (target > this.duration)
            target = this.duration;

        this.audio.currentTime = target;
        this._trigger('onTimeUpdate', target);
    }

    setVolume(value)
    {
        let vol = parseFloat(value);
        if (vol < 0)
            vol = 0;
        if (vol > 1)
            vol = 1;
        this.audio.volume = vol;
    }

    getStatus()
    {
        return {
            isPlaying: this.isPlaying,
            isPaused: this.isPaused,
            duration: this.duration,
            currentTime: this.audio.currentTime,
            volume: this.audio.volume
        };
    }

    on(event, callback)
    {
        if (event === 'onPcmProcess')
        {
            this.handlers.onPcmProcess = callback;
        }
        else if (this.handlers[event])
        {
            this.handlers[event].push(callback);
        }
    }

    _initAudioEvents()
    {
        this.audio.addEventListener('timeupdate', () =>
        {
            if (this.isPlaying)
            {
                this._trigger('onTimeUpdate', this.audio.currentTime);
            }
        });

        this.audio.addEventListener('ended', () =>
        {
            this._trigger('onEnded');
        });
    }

    _trigger(event, arg = null)
    {
        if (Array.isArray(this.handlers[event]))
        {
            this.handlers[event].forEach(cb => cb(arg));
        }
    }
}
