document.addEventListener('DOMContentLoaded', () =>
{
    const searchInput = document.querySelector('.search-input');
    const searchForm = document.querySelector('.search-form');

    if (!searchInput) return;

    if (searchForm)
    {
        searchForm.addEventListener('submit', (e) => e.preventDefault());
    }

    let debounceTimeout;

    searchInput.addEventListener('input', (event) =>
    {
        const query = event.target.value.trim();

        clearTimeout(debounceTimeout);

        debounceTimeout = setTimeout(async () =>
        {
            try
            {
                const tracksContainer = document.querySelector('.tracks-list-container');
                if (!tracksContainer) return;

                const htmlFragment = await window.TrackService.searchTracksHtml(query, 10);

                tracksContainer.outerHTML = htmlFragment;

            }
            catch (error)
            {
                console.error('Error updating tracks container:', error);
            }
        }, 300);
    });
});