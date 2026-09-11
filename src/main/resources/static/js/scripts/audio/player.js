import Track from './../../playlist/track.js';
import { getCsrfHeaders } from './../../super.js';

async function restoreLastSessionTrack()
{
    const lastTrackId = localStorage.getItem('lastPlayedTrackId');
    if (!lastTrackId)
        return;

    const lastTrackTime = localStorage.getItem('lastPlayedTrackPosition');
    if (!lastTrackTime)
        return;

    const lastVolume = localStorage.getItem('lastVolume');
    if (!lastVolume)
        return;

    const player = window.GlobalAudioPlayer;
    window.GlobalAudioPlayer.read(player);

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
            player.player.seek(lastTrackTime);

            const volumeInput = document.getElementById('player-volume');
            if (volumeInput)
            {
                volumeInput.value = lastVolume;
                const event = new Event('input',
                {
                    bubbles: true
                });
                volumeInput.dispatchEvent(event);
            }
        }
    }
}

async function restoreTrack()
{
    if(!window.trackIsRestored)
    {
        restoreLastSessionTrack()
            .then((track) =>
            {
              console.log("track:", track);
            })
            .catch((error) =>
            {
              console.error("error:", error);
            });

        window.trackIsRestored = true;
    }
}

if(!window.initPlayer)
{
    window.initPlayer = window.initPlayer || function (e)
    {
        restoreTrack();
    }

    document.addEventListener('SPADOMContentLoaded', initPlayer);
}

document.addEventListener('visibilitychange', () =>
{
    if (document.visibilityState === 'hidden')
    {
        const player = window.GlobalAudioPlayer;
        if(!player)
            throw new Error('err: !player');

        player.save();
    }
});