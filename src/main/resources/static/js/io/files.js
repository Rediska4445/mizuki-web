import { getCsrfHeaders } from '../super.js';

export async function sendMultipartRequest(url, formDataOrForm)
{
    const body = formDataOrForm instanceof FormData
        ? formDataOrForm
        : new FormData(formDataOrForm);

    const headers = getCsrfHeaders();

    const response = await fetch(url,
    {
        method: 'POST',
        headers: headers,
        body: body
    });

    if (!response.ok)
    {
        throw new Error(`Upload failed. Server responded with status ${response.status}`);
    }

    return response;
}