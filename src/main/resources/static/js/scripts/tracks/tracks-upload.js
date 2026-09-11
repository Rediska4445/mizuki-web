import { handleAll } from './tracks-upload-functions.js';

if(!window.initTrackUpload)
{
    window.initTrackUpload = window.initTrackUpload || function (e)
    {
        const target = (e.detail && e.detail.document) ? e.detail.document : document;

        handleAll(target);
    }

    document.addEventListener('SPADOMContentLoaded', initTrackUpload);
}