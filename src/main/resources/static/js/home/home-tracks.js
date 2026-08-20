import Track from './../playlist/track.js';

async function restoreLastSession()
{
    const lastTrackId = localStorage.getItem('lastPlayedTrackId');
    if (!lastTrackId)
        return;

    const player = window.GlobalAudioPlayer;
    const savedTrack = player.playlist.findTrackById(lastTrackId);

    if (savedTrack)
    {
        const targetIndex = player.playlist.getTracks().indexOf(savedTrack);
        if (targetIndex !== -1)
        {
            player.playlist.currentIndex = targetIndex;
        }

        if (player.player)
        {
            await player.loadTrack(savedTrack);
        }
    }
}

document.addEventListener('DOMContentLoaded', async () =>
{
    if (!window.GlobalAudioPlayer)
        return;

    const trackContainers = document.querySelectorAll('.track-main-content');
    const tracksPageList = Array.from(trackContainers)
        .map(container => Track.fromDOMElement(container))
        .filter(track => track !== null);

    window.GlobalAudioPlayer.playlist.setTracks(tracksPageList);

    await restoreLastSession();

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
                window.GlobalAudioPlayer.player.play();
            }
        });
    });
});
