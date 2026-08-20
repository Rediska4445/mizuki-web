import { AudioPlayer } from './../audio-player.js';
import Playlist from './../../playlist/playlist.js';
import Track from './../../playlist/track.js';

window.GlobalAudioPlayer =
{
    player: null,
    playBtn: null,
    nextBtn: null,
    prevBtn: null,
    progressInput: null,
    currentTimeSpan: null,
    durationSpan: null,
    currentTrackColor: '#ffffff',

    init()
    {
        this.player = new AudioPlayer();
        this.playlist = new Playlist();

        this.isSeeking = false;

        this.playBtn = document.getElementById('player-play-btn');
        this.nextBtn = document.getElementById('player-next-btn');
        this.prevBtn = document.getElementById('player-prev-btn');
        this.progressInput = document.getElementById('player-progress');
        this.currentTimeSpan = document.getElementById('player-current-time');
        this.durationSpan = document.getElementById('player-duration');

        this._bindEvents();
        this._initVolume();
    },

    async loadTrack(track)
    {
        localStorage.setItem('lastPlayedTrackId', track.trackId);

        if (track)
        {
            const coverImg = document.getElementById('now-playing-cover');
            const titleSpan = document.getElementById('now-playing-title');
            const authorSpan = document.getElementById('now-playing-author');

            if (coverImg && track.meta.cover)
                coverImg.src = track.meta.cover;
            if (titleSpan && track.meta.title)
                titleSpan.textContent = track.meta.title;
            if (authorSpan && track.meta.author)
                authorSpan.textContent = track.meta.author;

            this.currentTrackColor = track.meta.color;
        }

        if (this.progressInput)
        {
            this.progressInput.value = 0;
            this._updateSliderBackground(this.progressInput, 0, '#4d4d4d');
        }

        if (this.currentTimeSpan)
            this.currentTimeSpan.textContent = '0:00';
        if (this.durationSpan)
            this.durationSpan.textContent = '0:00';

        const targetTrack = this.playlist.findTrackById(track.trackId);
        if (targetTrack)
        {
            this.playlist.currentIndex = this.playlist.getTracks().indexOf(targetTrack);
        }

        await this.player.load(track.url);
    },

    async next()
    {
        const nextTrack = this.playlist.next();
        if (nextTrack)
        {
            await this.loadTrack(nextTrack);
            this.player.play();
        }
    },

    async previous()
    {
        const prevTrack = this.playlist.previous();
        if (prevTrack)
        {
            await this.loadTrack(prevTrack);
            this.player.play();
        }
    },

    _bindEvents()
    {
        this.player.on('onPlay', () =>
        {
            this.playBtn.textContent = '⏸';
        });

        this.player.on('onPause', () =>
        {
            this.playBtn.textContent = '▶';
        });

        this.prevBtn.addEventListener('click', async () =>
        {
            const prevTrack = this.playlist.previous();
            if (prevTrack)
            {
                await this.loadTrack(prevTrack);
                this.player.play();
            }
        });

        this.playBtn.addEventListener('click', () =>
        {
            const status = this.player.getStatus();
            if (status.isPlaying)
             {
                this.player.pause();
            }
            else
            {
                this.player.play();
            }
        });

        this.nextBtn.addEventListener('click', async () =>
        {
            const nextTrack = this.playlist.next();
            if (nextTrack)
            {
                await this.loadTrack(nextTrack);
                this.player.play();
            }
        });

        this.player.on('onTimeUpdate', (currentTime) =>
        {
            const status = this.player.getStatus();
            this.durationSpan.textContent = this._formatTime(status.duration);
            this.currentTimeSpan.textContent = this._formatTime(currentTime);

            if (status.duration > 0 && !this.isSeeking)
            {
                const percentage = (currentTime / status.duration) * 100;
                this.progressInput.value = percentage;
                this._updateSliderBackground(this.progressInput, percentage, this.currentTrackColor);
            }
        });

        this.progressInput.addEventListener('input', (e) =>
        {
            this.isSeeking = true;

            const status = this.player.getStatus();
            if (status.duration > 0)
            {
                const percentage = e.target.value;
                const targetSeconds = (percentage / 100) * status.duration;

                this.currentTimeSpan.textContent = this._formatTime(targetSeconds);
                this._updateSliderBackground(this.progressInput, percentage, this.currentTrackColor);
            }
        });

        this.progressInput.addEventListener('change', (e) =>
        {
            const status = this.player.getStatus();
            if (status.duration > 0)
            {
                const percentage = e.target.value;
                const targetSeconds = (percentage / 100) * status.duration;

                this.player.seek(targetSeconds);
            }
        });
    },

    _initVolume()
    {
        const volumeInput = document.getElementById('player-volume');
        if (volumeInput)
        {
            this._updateSliderBackground(volumeInput, volumeInput.value * 100, "#ffffff");

            volumeInput.addEventListener('input', (e) =>
            {
                const percentage = e.target.value * 100;
                this._updateSliderBackground(volumeInput, percentage, "#ffffff");

                if (this.player && typeof this.player.setVolume === 'function')
                {
                    this.player.setVolume(e.target.value);
                }
            });
        }
    },

    _updateSliderBackground(slider, percentage, color)
    {
        slider.style.background = `linear-gradient(to right, ${color} ${percentage}%, #4d4d4d ${percentage}%)`;
    },

    _formatTime(seconds)
    {
        if (isNaN(seconds))
            return '0:00';

        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);

        return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    }
};

window.GlobalAudioPlayer.init();