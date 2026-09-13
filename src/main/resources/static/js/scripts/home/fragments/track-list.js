import Track from './../../../playlist/track.js';

function updatePlaylistFromDOM()
{
    const trackContainers = document.querySelectorAll('.track-main-content');
    const tracksPageList = Array.from(trackContainers)
        .map(container => Track.fromDOMElement(container))
        .filter(track => track !== null);

    if (window.GlobalAudioPlayer && window.GlobalAudioPlayer.playlist)
    {
        window.GlobalAudioPlayer.playlist.setTracks(tracksPageList);
    }
}

async function handleTrackClick(event)
{
    if (!window.GlobalAudioPlayer)
        return;

    const button = event.target.closest('.track-item-player-button');
    if (!button) return;

    const mainContent = button.closest('.track-main-content');
    if (!mainContent) return;

    const trackId = mainContent.getAttribute('data-track-id');
    const targetTrack = window.GlobalAudioPlayer.playlist.findTrackById(trackId);

    if (targetTrack)
    {
        await window.GlobalAudioPlayer.loadTrack(targetTrack);
        window.GlobalAudioPlayer.player.play();
    }
    else
    {
        updatePlaylistFromDOM();

        await window.GlobalAudioPlayer.loadTrack(targetTrack);
        window.GlobalAudioPlayer.player.play();
    }
}

// Isolate it to ownself context
window.TrackList =
{
    updatePlaylistFromDOM: updatePlaylistFromDOM,
    handleTrackClick: handleTrackClick
};