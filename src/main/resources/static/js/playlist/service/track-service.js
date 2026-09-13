import { getCsrfHeaders } from './../../super.js';
import * as mm from './../../libs/music-metadata.js';

async function searchTracksHtml(query = '', size = 10)
{
    const url = `/tracks/search?query=${encodeURIComponent(query)}&size=${size}`;
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
    const url = `/tracks/search?query=${encodeURIComponent(query)}&size=${size}`;
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

async function extractMetadata(file)
{
    if (!file)
    {
        throw new Error('File is required');
    }

    const parsed = await mm.parseBlob(file,
    {
        contentType: "audio/mpeg; charset=utf-8"
    });
    const common = parsed.common;
    const format = parsed.format;

    const response =
    {
        "title": common.title || '',
        "artist": common.artist || '',
        "duration": format.duration ? Math.round(format.duration) : 0,
        "explicit": false
    };

    if (common.picture && common.picture.length > 0)
    {
        const picture = common.picture[0];

        const blob = new Blob([picture.data], { type: picture.format });
        response["cover"] = URL.createObjectURL(blob);
    }
    else
    {
        response["cover"] = null;
    }

    return response;
}

function getAudioDuration(file)
{
    return new Promise((resolve, reject) =>
    {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const reader = new FileReader();

        reader.onload = function(e)
        {
            audioContext.decodeAudioData(e.target.result, function(buffer)
            {
                resolve(Math.round(buffer.duration));
            }, reject);
        };
        reader.onerror = reject;
        reader.readAsArrayBuffer(file);
    });
}

async function extractMetadataFromServer(file)
{
    if (!file)
    {
        throw new Error('File is required for metadata extraction');
    }

    const formData = new FormData();
    formData.append('file', file);

    const headers = getCsrfHeaders();

    const response = await fetch('/tracks/extract',
    {
        method: 'POST',
        headers: headers,
        body: formData
    });

    if (!response.ok)
    {
        const errorText = await response.text();
        throw new Error(`Server error (${response.status}): ${errorText || response.statusText}`);
    }

    return await response.json();
}

export
{
    extractMetadata,
    getAudioDuration,
    searchTracksHtml,
    searchTracksJson
};