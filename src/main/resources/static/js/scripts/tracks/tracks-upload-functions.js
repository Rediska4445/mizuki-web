import { getCsrfHeaders } from './../../super.js';
import { extractMetadata } from './../../playlist/service/track-service.js';
import { go, goUrl } from './../../io/network.js';

function fillFormWithMetadata(target, metadata)
{
    if (!metadata)
        return;

    const titleInput = target.querySelector('#title');
    const artistInput = target.querySelector('#artist');
    const durationInput = target.querySelector('#duration');
    const explicitCheckbox = target.querySelector('#explicit');
    const coverImage = target.querySelector('#cover');

    if (titleInput && metadata.title)
    {
        titleInput.value = metadata.title;
    }
    if (artistInput && metadata.artist)
    {
        artistInput.value = metadata.artist;
    }
    if (durationInput && metadata.duration)
    {
        durationInput.value = Math.round(metadata.duration);
    }
    if (explicitCheckbox)
    {
        explicitCheckbox.checked = Boolean(metadata.explicit);
    }
    if (coverImage && metadata.cover)
    {
        coverImage.src = metadata.cover;
        coverImage.classList.add('active');
    }
}

function handleCoverUpload(target)
{
    const coverInput = target.querySelector('#coverFile');
    const coverImage = target.querySelector('#cover');

    if (!coverInput || !coverImage) return;

    coverInput.addEventListener('change', (event) =>
    {
        const file = event.target.files[0];
        if (!file) return;

        coverImage.src = URL.createObjectURL(file);
        coverImage.classList.add('active');
    });
}

function handleAudioUploadAndMetadata(target)
{
    const audioInput = target.querySelector('#audioFile');
    const audioPreview = target.querySelector('#audioPreview');
    const audioText = target.querySelector('#audioText');

    if (!audioInput || !audioPreview || !audioText)
        return;

    audioInput.addEventListener('change', async (event) =>
    {
        const file = event.target.files[0];
        if (!file) return;

        audioText.textContent = `${file.name}`;
        audioPreview.classList.add('audio-selected');

        try
        {
            const metadata = await extractMetadata(file);

            fillFormWithMetadata(target, metadata);
        }
        catch (error)
        {
            console.error('Failed to extract metadata:', error.message);
        }
    });
}

function handleFormSubmit(target)
{
    const form = target.querySelector('.submit-btn')?.closest('form');
    if (!form) return;

    form.addEventListener('submit', async (event) =>
    {
        event.preventDefault();

        try
        {
            const formData = new FormData();

            formData.append('title', target.querySelector('#title')?.value || '');
            formData.append('authors', target.querySelector('#artist')?.value || '');

            const durationSec = parseInt(target.querySelector('#duration')?.value || '0', 10);
            formData.append('duration', durationSec * 1000);

            const isExplicit = target.querySelector('#explicit')?.checked || false;
            formData.append('explicit', isExplicit.toString());

            const audioInput = target.querySelector('#audioFile');
            if (audioInput && audioInput.files.length > 0)
            {
                const file = audioInput.files[0];
                formData.append('file', file, file.name);
            }
            else
            {
                throw new Error(`formData error`);
            }

            const coverImage = target.querySelector('#cover');

            if(coverImage.src == null)
                throw new Error(`coverImage is null`);

            const responseBlob = await fetch(coverImage.src);
            const blob = await responseBlob.blob();
            formData.append('cover', blob, "auto_cover.jpg");

            const response = await fetch('/tracks/upload',
            {
                method: 'POST',
                headers: getCsrfHeaders(),
                body: formData
            });

            if (!response.ok)
            {
                throw new Error(`Server responded with status ${response.status}`);
            }
            if (response.redirected)
            {
                go(response.url);
            }
        }
        catch (error)
        {
            console.error('Submission failed:', error.message);
        }
    });
}

export function handleAll(target)
{
    handleCoverUpload(target);
    handleAudioUploadAndMetadata(target);
    handleFormSubmit(target);
}