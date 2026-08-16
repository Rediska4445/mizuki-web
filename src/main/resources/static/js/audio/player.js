window.BottomPlayer =
{
    audio: null,
    fader: null,
    currentTrackId: null,
    currentTrackColor: "#ffffff",
    isLoadingNewTrack: false,
    isDragging: false,

    updateSliderBackground(input, percentage)
    {
        const color = this.currentTrackColor || '#007bff';
        input.style.background = `linear-gradient(to right, ${color} ${percentage}%, #4d4d4d ${percentage}%)`;
    },

    init: function()
    {
        if (!this.audio)
        {
            this.audio = new Audio();
            this.fader = new AudioFader(this.audio, 200);
            this.initEvents();
            this.initDomElements();
        }
    },

    initEvents: function()
    {
        const progressInput = document.getElementById("player-progress");

        if (progressInput)
        {
            progressInput.addEventListener('input', (e) =>
            {
                const percentage = e.target.value;
                this.updateSliderBackground(progressInput, percentage);

                const currentTimeSpan = document.getElementById("player-current-time");
                if (currentTimeSpan && this.audio.duration)
                {
                    const calculatedTime = (percentage / 100) * this.audio.duration;
                    currentTimeSpan.textContent = this.formatTime(calculatedTime);
                }
            });
        }

        this.audio.addEventListener('timeupdate', () =>
        {
            if (this.isLoadingNewTrack || this.isDragging)
                return;

            const progressInput = document.getElementById("player-progress");
            const currentTimeSpan = document.getElementById("player-current-time");

            if (this.audio.duration && !isNaN(this.audio.duration) && isFinite(this.audio.duration))
            {
                const percentage = (this.audio.currentTime / this.audio.duration) * 100;

                if (progressInput)
                {
                    progressInput.value = percentage;
                    this.updateSliderBackground(progressInput, percentage);
                }
            }

            if (currentTimeSpan)
            {
                currentTimeSpan.textContent = this.formatTime(this.audio.currentTime);
            }
        });

        this.audio.addEventListener('ended', () =>
        {
            this.playNextTrack();
        });

        const updateDuration = () =>
        {
            const durationSpan = document.getElementById("player-duration");
            if (durationSpan && this.audio.duration && !isNaN(this.audio.duration) && isFinite(this.audio.duration))
            {
                durationSpan.textContent = this.formatTime(this.audio.duration);
                this.isLoadingNewTrack = false;
            }
        };

        this.audio.addEventListener('durationchange', updateDuration);
        this.audio.addEventListener('loadedmetadata', updateDuration);

        this.audio.addEventListener('play', () => this.updatePlayButton(true));
        this.audio.addEventListener('pause', () => this.updatePlayButton(false));
        this.audio.addEventListener('ended', () => this.updatePlayButton(false));
        this.audio.addEventListener('error', (e) =>
        {
            console.error(e);
            this.isLoadingNewTrack = false;
        });
    },

    initDomElements: function()
    {
        const attachListeners = () =>
        {
            const playBtn = document.getElementById("player-play-btn");
            const prevBtn = document.getElementById("player-prev-btn");
            const nextBtn = document.getElementById("player-next-btn");
            const progressInput = document.getElementById("player-progress");
            const volumeInput = document.getElementById("player-volume");

            if (playBtn)
                playBtn.onclick = () => this.togglePlay();
            if (prevBtn)
                prevBtn.onclick = () => this.playPrevTrack();
            if (nextBtn)
                nextBtn.onclick = () => this.playNextTrack();

            if (progressInput)
            {
                progressInput.oninput = (e) =>
                {
                    this.seek(e.target.value);
                    e.target.style.background = `linear-gradient(to right, ${this.currentTrackColor} ${e.target.value}%, #4d4d4d ${e.target.value}%)`;
                };
            }

            if (volumeInput)
            {
                const volPct = volumeInput.value * 100;
                volumeInput.style.background = `linear-gradient(to right, #ffffff ${volPct}%, #4d4d4d ${volPct}%)`;

                volumeInput.oninput = (e) =>
                {
                    this.setVolume(volumeValue);
                    this.fader.setTargetVolume(volumeValue);
                    const pct = e.target.value * 100;
                    e.target.style.background = `linear-gradient(to right, #ffffff ${pct}%, #4d4d4d ${pct}%)`;
                };
            }
        };

        if (document.readyState === "loading")
        {
            document.addEventListener("DOMContentLoaded", attachListeners);
        }
        else
        {
            attachListeners();
        }
    },

    playNextTrack: function()
    {
        const targetContext = (window.frames && window.frames["site-frame"]) ? window.frames["site-frame"].document : document;
        const buttons = Array.from(targetContext.querySelectorAll(".track-item-player-button"));
        if (buttons.length === 0)
            return;

        const currentAttr = this.currentTrackId;
        const currentIndex = buttons.findIndex(btn =>
        {
            const id = btn.getAttribute("data-track-id") || btn.dataset.trackId || btn.getAttribute("th:data-track-id");
            return String(id) === String(currentAttr);
        });

        if (currentIndex !== -1 && currentIndex + 1 < buttons.length)
        {
            buttons[currentIndex + 1].click();
        }
        else if (currentIndex >= buttons.length)
        {
            buttons[currentIndex = 0].click();
        }
    },

    playPrevTrack: function()
    {
        const targetContext = (window.frames && window.frames["site-frame"]) ? window.frames["site-frame"].document : document;
        const buttons = Array.from(targetContext.querySelectorAll(".track-item-player-button"));
        if (buttons.length === 0)
            return;

        const currentAttr = this.currentTrackId;
        const currentIndex = buttons.findIndex(btn =>
        {
            const id = btn.getAttribute("data-track-id") || btn.dataset.trackId || btn.getAttribute("th:data-track-id");
            return String(id) === String(currentAttr);
        });

        if (currentIndex > 0)
        {
            buttons[currentIndex - 1].click();
        }
        else if(currentIndex <= 0)
        {
            buttons[currentIndex = buttons.length - 1].click();
        }
    },

    playTrack: function(trackId)
     {
        this.init();

        if (this.currentTrackId === trackId)
        {
            this.togglePlay();
            return;
        }

        this.currentTrackId = trackId;
        this.isLoadingNewTrack = true;

        const progressInput = document.getElementById("player-progress");
        const currentTimeSpan = document.getElementById("player-current-time");
        if (progressInput)
        {
            progressInput.value = 0;
            progressInput.style.background = `linear-gradient(to right, #4d4d4d 0%, #4d4d4d 0%)`;
        }
        if (currentTimeSpan)
            currentTimeSpan.textContent = "0:00";

        fetch(`/track/${trackId}`)
            .then(res => res.json())
            .then(track => {
                const titleElement = document.getElementById("now-playing-title");
                const authorElement = document.getElementById("now-playing-author");
                const coverElement = document.getElementById("now-playing-cover");
                const durationSpan = document.getElementById("player-duration");

                if (titleElement) titleElement.textContent = track.title || "Без названия";
                if (authorElement) authorElement.textContent = track.author || "Неизвестен";
                if (coverElement && track.cover) coverElement.src = track.cover;
                if (durationSpan) durationSpan.textContent = "--:--";

                this.currentTrackColor = track.color || "#ffffff";

                if (progressInput) {
                    progressInput.style.background = `linear-gradient(to right, ${this.currentTrackColor} 0%, #4d4d4d 0%)`;
                }

                this.audio.src = `/track/stream/${trackId}`;
                this.audio.play().catch(err => console.error(err));
            })
            .catch(err => {
                console.error(err);
                this.isLoadingNewTrack = false;
            });
    },

    togglePlay: function() {
        if (!this.audio || !this.currentTrackId)
            return;

        if (this.audio.paused)
        {
            this.fader.fadeIn();
        }
        else
        {
            this.fader.fadeOut();
        }
    },

    seek: function(value) {
        if (!this.audio || !this.audio.duration || isNaN(this.audio.duration) || !isFinite(this.audio.duration)) return;
        this.audio.currentTime = (value / 100) * this.audio.duration;
    },

    setVolume: function(value) {
        this.init();
        this.audio.volume = value;
    },

    updatePlayButton: function(isPlaying) {
        const btn = document.getElementById("player-play-btn");
        if (btn) btn.textContent = isPlaying ? "❚❚" : "▶";
    },

    formatTime: function(seconds) {
        if (isNaN(seconds) || !isFinite(seconds)) return "0:00";
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    }
};

window.BottomPlayer.init();
