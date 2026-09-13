document.addEventListener("click", function(event) {
    const button = event.target.closest(".track-item-player-button");
    if (!button) return;

    const trackId = button.getAttribute("data-track-id")
                 || button.dataset.trackId
                 || button.getAttribute("th:data-track-id");

    if (!trackId) {
        console.error("ID трека не найден в атрибутах кнопки!", button);
        return;
    }

    const targetPlayer = (window.parent && window.parent.BottomPlayer) ? window.parent.BottomPlayer : window.BottomPlayer;

    if (targetPlayer && typeof targetPlayer.playTrack === "function")
    {
        targetPlayer.playTrack(trackId);
    }
    else
    {
        console.error("Глобальный плеер BottomPlayer не найден ни в текущем окне, ни в parent!");
    }
});
