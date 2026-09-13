import Track from './../../playlist/track.js';
import { getCsrfHeaders } from './../../super.js';
import { handleHeader } from './header/header.js';

async function handleTrackLike(e)
{
    const likeButton = e.target.closest('.track-like-button');
    if (!likeButton || likeButton.disabled)
        throw new Error('err: !likeButton || likeButton.disabled');

    e.preventDefault();

    const trackItem = likeButton.closest('.track-item');
    if (!trackItem)
        throw new Error('err: !trackItem');

    const mainContent = trackItem.querySelector('.track-main-content');
    if (!mainContent)
        throw new Error('err: !mainContent');

    const trackId = mainContent.getAttribute('data-track-id');

    likeButton.classList.toggle('active');
    likeButton.disabled = true;

    const isLike = likeButton.classList.contains('active');

    try
    {
        const csrfHeaders = getCsrfHeaders();
        const response = await fetch('/tracks/like',
        {
            method: 'POST',
            headers:
            {
                'Content-Type': 'application/x-www-form-urlencoded',
                ...csrfHeaders
            },
            body: `trackId=${encodeURIComponent(trackId)}&isLike=${encodeURIComponent(isLike)}`
        });

        if (!response.ok)
            throw new Error('response: ' + response);
    }
    catch (error)
    {
        likeButton.classList.toggle('active');

        throw new Error('err: ' + error);
    }
    finally
    {
        likeButton.disabled = false;
    }
}

document.addEventListener('click', async (e) =>
{
    const likeButton = e.target.closest('.track-like-button, #track-like-button');

    if (!likeButton)
        return;

    await handleTrackLike(e);

    likeButton.classList.toggle('is-active');
});

console.log(window.initHome);

if(!window.initHome)
{
    window.initHome = window.initHome || function (e)
    {
        const target = (e.detail && e.detail.document) ? e.detail.document : document;

        const currentPath = window.location.pathname;

        if (currentPath !== '/')
            return;

        handleHeader(target);

        if (!window.GlobalAudioPlayer || !window.TrackList)
            return;

        window.TrackList.updatePlaylistFromDOM();

        const staticWrapper = target.querySelector('.tracks-player-wrapper');
        if (staticWrapper)
        {
            staticWrapper.addEventListener('click', (e) =>
            {
                window.TrackList.handleTrackClick(e);
            });

            const observer = new MutationObserver((mutationsList) =>
            {
                for (const mutation of mutationsList)
                {
                    if (mutation.type === 'childList')
                    {
                        window.TrackList.updatePlaylistFromDOM();

                        break;
                    }
                }
            });

            observer.observe(staticWrapper, { childList: true, subtree: false });
        }
    }

    document.addEventListener('SPADOMContentLoaded', window.initHome);
}