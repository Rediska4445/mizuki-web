import Track from './../playlist/track.js';

document.addEventListener('DOMContentLoaded', () =>
{
    if (!window.GlobalAudioPlayer)
        return;

    const trackContainers = document.querySelectorAll('.track-main-content');
    const tracksPageList = Array.from(trackContainers)
        .map(container => Track.fromDOMElement(container))
        .filter(track => track !== null);

    window.GlobalAudioPlayer.playlist.setTracks(tracksPageList);

    const trackButtons = document.querySelectorAll('.track-item-player-button');
    trackButtons.forEach(button =>
    {
        button.addEventListener('click', async () =>
        {
            const mainContent = button.closest('.track-main-content');
            const trackId = mainContent.getAttribute('data-track-id');

            const targetTrack = window.GlobalAudioPlayer.playlist.findTrackById(trackId);

            if (targetTrack)
            {
                await window.GlobalAudioPlayer.loadTrack(targetTrack);
            }
        });
    });
});
