async function searchTracksHtml(query = '', size = 10)
{
    const url = `/user/tracks/search?query=${encodeURIComponent(query)}&size=${size}`;
    const response = await fetch(url,
    {
        method: 'GET',
        headers:
        {
            'Accept': 'text/html'
        }
    });
    return await response.text();
}

async function searchTracksJson(query = '', size = 10)
{
    const url = `/user/tracks/search?query=${encodeURIComponent(query)}&size=${size}`;
    const response = await fetch(url,
    {
        method: 'GET',
        headers:
        {
            'Accept': 'application/json'
        }
    });
    return await response.json();
}

window.TrackService =
{
    searchTracksHtml: searchTracksHtml,
    searchTracksJson: searchTracksJson
};