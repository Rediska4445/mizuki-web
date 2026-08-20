import Track from './Track.js';

class Playlist
{
    constructor(initialTracks = [])
    {
        this.tracks = [];
        this.currentIndex = -1;

        if (Array.isArray(initialTracks))
        {
            this.tracks = initialTracks.filter(track => track && track.url);
            if (this.tracks.length > 0)
            {
                this.currentIndex = 0;
            }
        }
    }

    getCurrentTrack()
    {
        if (this.currentIndex < 0 || this.currentIndex >= this.tracks.length)
        {
            return null;
        }

        return this.tracks[this.currentIndex];
    }

    findTrackById(trackId)
    {
        if (!trackId)
            return null;

        return this.tracks.find(track => track.trackId === trackId) || null;
    }

    next()
    {
        if (this.tracks.length === 0)
        {
            return null;
        }

        if (this.currentIndex < this.tracks.length - 1)
        {
            this.currentIndex++;
        }
        else
        {
            this.currentIndex = 0;
        }

        return this.getCurrentTrack();
    }

    previous()
    {
        if (this.tracks.length === 0)
        {
            return null;
        }

        if (this.currentIndex > 0)
        {
            this.currentIndex--;
        }
        else
        {
            this.currentIndex = this.tracks.length - 1;
        }

        return this.getCurrentTrack();
    }

    addTrack(track)
    {
        if (!track || !track.url)
        {
            return;
        }

        this.tracks.push(track);
        if (this.currentIndex === -1)
        {
            this.currentIndex = 0;
        }
    }

    removeTrack(track)
    {
        if (!track || !track.url || this.tracks.length === 0)
         {
            return false;
        }

        const currentTrackBeforeRemove = this.getCurrentTrack();
        const indexToRemove = this.tracks.findIndex(t => t.url === track.url);

        if (indexToRemove === -1)
         {
            return false;
        }

        this.tracks.splice(indexToRemove, 1);

        if (this.tracks.length === 0)
         {
            this.currentIndex = -1;
            return true;
        }

        if (currentTrackBeforeRemove && currentTrackBeforeRemove.url === track.url)
        {
            if (this.currentIndex >= this.tracks.length)
            {
                this.currentIndex = 0;
            }
        }
        else
        {
            this.currentIndex = this.tracks.findIndex(t => t.url === currentTrackBeforeRemove.url);
        }
        return true;
    }

    setTracks(newTracks)
     {
        const currentTrackBeforeUpdate = this.getCurrentTrack();
        this.tracks = [];

        if (Array.isArray(newTracks))
        {
            this.tracks = newTracks.filter(track => track && track.url);
        }

        if (this.tracks.length === 0)
        {
            this.currentIndex = -1;
            return;
        }

        const newIndex = this.tracks.findIndex(t => currentTrackBeforeUpdate && t.url === currentTrackBeforeUpdate.url);
        if (newIndex !== -1)
        {
            this.currentIndex = newIndex;
        }
        else
        {
            this.currentIndex = 0;
        }
    }

    clear()
    {
        this.tracks = [];
        this.currentIndex = -1;
    }

    getTracks()
    {
        return [...this.tracks];
    }

    getCurrentIndex()
    {
        return this.currentIndex;
    }

    size()
    {
        return this.tracks.length;
    }
}

export default Playlist;