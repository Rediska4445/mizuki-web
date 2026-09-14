import { searchTracksHtml } from './../../../playlist/service/track-service.js';
import { handleUserMenu } from './user-menu.js';

export function handleHeader(target)
{
    handleUserMenu(target);

    const searchInput = target.querySelector('.search-input');
    const searchForm = target.querySelector('.search-form');

    if (!searchInput)
        return;

    if (searchForm)
    {
        searchForm.addEventListener('submit', (event) =>
        {
            e.preventDefault();
        });
    }

    let debounceTimeout;

    searchInput.addEventListener('input', (event) =>
    {
        const query = event.target.value;

        clearTimeout(debounceTimeout);

        debounceTimeout = setTimeout(async () =>
        {
            try
            {
                const tracksContainer = document.querySelector('.tracks-list-container');
                if (!tracksContainer)
                    return;

                const htmlFragment = await searchTracksHtml(query, 10);

                tracksContainer.outerHTML = htmlFragment;
            }
            catch (error)
            {
                console.error('Error updating tracks container:', error);
            }
        }, 300);
    });
}