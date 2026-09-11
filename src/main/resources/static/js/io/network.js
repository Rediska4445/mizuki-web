export function go(url)
{
    if (!url) return;

    if (typeof htmx !== 'undefined')
    {
        htmx.ajax('GET', url,
        {
            target: 'body',
            swap: 'innerHTML',
            values: { 'hx-push-url': 'true' }
        });

        window.scrollTo(0, 0);
    }
    else
    {
        window.location.href = url;
    }
}

export function goUrl(url, safeContext = null)
{
    if (!url) return;

    if (typeof htmx !== 'undefined')
    {
        htmx.ajax('GET', url,
        {
            target: 'body',
            swap: 'innerHTML',
            select: 'body',
        });

        if (window.history && window.history.pushState)
        {
            window.history.pushState({}, '', url);
        }

        window.scrollTo(0, 0);
    }
    else
    {
        window.location.href = url;
    }
}