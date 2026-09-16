let isFirstLoad = true;
window.spaLabelsController = null;

htmx.config.globalViewTransitions = true;

export function getCsrfHeaders()
{
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    
    return token && header ? { [header]: token } : {};
}

document.addEventListener('htmx:load', (event) =>
{
    if (!event || !event.detail || !event.detail.elt)
        return;

    if (event.detail.elt === document.body && !isFirstLoad)
        return;

    isFirstLoad = false;

    if (window.spaLabelsController)
    {
        window.spaLabelsController.abort();
    }

    window.spaLabelsController = new AbortController();

    document.addEventListener('SPADOMContentLoaded', window.initHome,
    {
        signal: window.spaLabelsController.signal
    });

    // legacy code
    window.myAppInitialized = false;
    window.myAppInitialized2 = false;

    const currentTransitionId = Date.now();

    const spaEvent = new CustomEvent('SPADOMContentLoaded',
    {
        bubbles: true,
        cancelable: true,
        detail:
        {
            document: event.detail.elt,
            transitionId: currentTransitionId
        }
    });

    document.dispatchEvent(spaEvent);
});