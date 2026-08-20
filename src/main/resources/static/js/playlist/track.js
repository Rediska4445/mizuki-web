class Track
 {
    constructor(url, trackId, meta = null)
    {
        if (!url || typeof url !== 'string' || url.trim() === '')
        {
            throw new Error('Track URL must be a non-empty string');
        }

        this.url = url;
        this.trackId = trackId;
        this.meta =
        {
            title: meta?.title || 'Choose track',
            author: meta?.author || 'Im boring',
            cover: meta?.cover || '/img/logo-hd.png',
            color: meta?.color || '#ffffff'
        };
    }

    static fromDOMElement(container)
    {
        if (!container)
            return null;

        const trackId = container.getAttribute('data-track-id');
        const trackUrl = container.getAttribute('data-track-url');

        const metaInfo = container.querySelector('.track-meta-info');
        const trackColor = metaInfo ? metaInfo.getAttribute('data-track-color') : '#ffffff';

        const coverEl = container.querySelector('.track-cover-img');
        const titleEl = container.querySelector('.track-title-text');
        const artistEl = container.querySelector('.track-artist-text');

        const meta = {
            title: titleEl ? titleEl.textContent.trim() : 'Unknown Title',
            author: artistEl ? artistEl.textContent.trim() : 'Unknown Artist',
            cover: coverEl ? coverEl.getAttribute('src') : '/img/logo-hd.png',
            color: trackColor
        };

        return new Track(trackUrl, trackId, meta);
    }
}

export default Track;
