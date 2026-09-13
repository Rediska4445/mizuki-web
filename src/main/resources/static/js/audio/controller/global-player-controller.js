import { getCsrfHeaders } from './../../super.js';
import { AudioPlayer } from './../audio-player.js';
import { fade } from './../fader.js';
import Playlist from './../../playlist/playlist.js';
import Track from './../../playlist/track.js';

const ICON_PLAY = '▶';
const ICON_PAUSE = '⏸';

function sendListenEventToServer(trackId)
{
    fetch(`/audio/stream/${trackId}/complete`,
    {
        method: 'POST',
        headers:
        {
            'Content-Type': 'application/json',
            ...getCsrfHeaders()
        }
    })
    .catch(error => console.error("error sending audio complete request:", error));
};

window.GlobalAudioPlayer =
{
    player: null,
    playlist: null,
    globalPause: false,
    playBtn: null,
    nextBtn: null,
    prevBtn: null,
    progressInput: null,
    currentTimeSpan: null,
    durationSpan: null,
    playerVolume: 0.0,
    fadeDurationSeconds: 0.200,
    currentTrackColor: '#ffffff',
    isTracked: false,
    listeningThreshold: null,
    currentTrackId: null,

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
            this.durationSpan.textContent = '--:--';

        const targetTrack = this.playlist.findTrackById(track.trackId);
        if (targetTrack)
        {
            this.playlist.currentIndex = this.playlist.getTracks().indexOf(targetTrack);
        }

        this.currentTrackId = track.trackId;

        await this.player.load(track.url);
        this.player.setVolume(this.playerVolume);

        this.listeningThreshold = null;
        this.isTracked = false;
    },

    async next()
    {
        const nextTrack = this.playlist.next();
        if (nextTrack)
        {
            await this.loadTrack(nextTrack);
            if(!this.globalPause)
            {
                this.player.play();
            }
        }
    },

    async previous()
    {
        const prevTrack = this.playlist.previous();
        if (prevTrack)
        {
            await this.loadTrack(prevTrack);
            if(!this.globalPause)
            {
                this.player.play();
            }
        }
    },

    save()
    {
        if (!this.player)
        {
            throw new Error('Failed to save this.player state: this.player object is missing.');
        }
        if (!this.player?.audio)
        {
            throw new Error('Failed to save this.player state: this.player.player.audio element is missing.');
        }
        if (!this.playlist || typeof this.playlist.serialize !== 'function')
        {
            throw new Error('Failed to save this.player state: playlist is missing or doesn\'t have a serialize method.');
        }

        localStorage.setItem('lastPlayedTrackId', this.playlist.getCurrentTrack().trackId);
        localStorage.setItem('lastPlayedTrack', this.playlist.getCurrentTrack().serialize());
        localStorage.setItem('lastPlayedTrackPosition', this.player.audio.currentTime);
        localStorage.setItem('lastVolume', this.playerVolume);
        localStorage.setItem('lastPlaylist', this.playlist.serialize());
    },

    // static function
    read(existingPlayer = null)
    {
        const rawPosition = localStorage.getItem('lastPlayedTrackPosition');
        const rawVolume = localStorage.getItem('lastVolume');
        const rawTrack = localStorage.getItem('lastPlayedTrack');
        const rawPlaylist = localStorage.getItem('lastPlaylist');

        if (rawPosition === null || rawVolume === null)
        {
            throw new Error('Failed to restore player state: One or more localStorage keys are missing.');
        }

        const volume = parseFloat(rawVolume);
        if (isNaN(volume))
        {
            throw new Error(`Failed to restore player state: lastVolume in localStorage is not a valid number ("${rawVolume}").`);
        }

        const position = parseFloat(rawPosition);
        if (isNaN(position))
        {
            throw new Error(`Failed to restore player state: lastPlayedTrackPosition in localStorage is not a valid number ("${rawPosition}").`);
        }

        let targetPlayer;

        if (existingPlayer !== null && existingPlayer !== undefined)
        {
            targetPlayer = existingPlayer;
        }
        else
        {
            targetPlayer = new this();
            targetPlayer.init();
        }

        if (!targetPlayer.player)
        {
            throw new Error('Failed to restore player state: targetPlayer.player object is missing.');
        }
        if (!targetPlayer.player.audio)
        {
            throw new Error('Failed to restore player state: targetPlayer.player.audio element is missing.');
        }

        if(rawPlaylist)
            targetPlayer.playlist = Playlist.deserialize(rawPlaylist);

        targetPlayer.playerVolume = volume;
        targetPlayer.player.audio.currentTime = position;

        targetPlayer.loadTrack(JSON.parse(rawTrack));

        return targetPlayer;
    },

    _bindEvents()
    {
        this.player.on('onPlay', () =>
        {
            if(this.playBtn.textContent !== ICON_PAUSE)
                this.playBtn.textContent = ICON_PAUSE;

            this.globalPause = false;
        });

        this.player.on('onPause', () =>
        {
            if(this.playBtn.textContent !== ICON_PLAY)
                this.playBtn.textContent = ICON_PLAY;

            this.globalPause = true;
        });

        this.player.on('onEnded', () =>
        {
            this.nextBtn.click();
        });

        this.prevBtn.addEventListener('click', async () =>
        {
            await this.previous();
        });

        this.playBtn.addEventListener('click', () =>
        {
            const status = this.player.getStatus();
            if (status.isPlaying)
            {
                this.playBtn.textContent = ICON_PLAY;

                fade(this.player.audio.volume, 0, this.fadeDurationSeconds, (v) => this.player.setVolume(v))
                    .then(() => this.player.pause());
            }
            else
            {
                this.playBtn.textContent = ICON_PAUSE;

                this.player.play();

                fade(this.player.audio.volume, this.playerVolume, this.fadeDurationSeconds, (v) => this.player.setVolume(v));
            }
        });

        this.nextBtn.addEventListener('click', async () =>
        {
            await this.next();
        });

        this.player.on('onTimeUpdate', (currentTime) =>
        {
            const status = this.player.getStatus();

            if (status.duration > 0 && !this.isSeeking)
            {
                this.durationSpan.textContent = this._formatTime(status.duration);
                this.currentTimeSpan.textContent = this._formatTime(currentTime);

                const percentage = (currentTime / status.duration) * 100;
                this.progressInput.value = percentage;
                this._updateSliderBackground(this.progressInput, percentage, this.currentTrackColor);
            }

            if (this.listeningThreshold === null && this.player.audio.duration > 0)
            {
                const restoredTime = this.player.audio.currentTime;
                const totalDuration = this.player.audio.duration;
                const standardThreshold = totalDuration / 10;

                if (restoredTime < standardThreshold)
                {
                    this.listeningThreshold = standardThreshold;
                }
                else
                {
                    const remainingDuration = totalDuration - restoredTime;
                    this.listeningThreshold = restoredTime + (remainingDuration / 10);
                }
            }

            if (this.listeningThreshold !== null && this.player.audio.currentTime >= this.listeningThreshold && !this.isTracked)
            {
                this.isTracked = true;

                sendListenEventToServer(this.currentTrackId);
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
            this.isSeeking = false;

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
                    this.player.setVolume(this.playerVolume = e.target.value);
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