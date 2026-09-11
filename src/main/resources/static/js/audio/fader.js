export const fade = (fromVolume, toVolume, duration, onVolumeChange) =>
{
    return new Promise((resolve) =>
    {
        const start = parseFloat(fromVolume);
        const target = parseFloat(toVolume);
        const startTime = performance.now();
        const msDuration = duration * 1000;

        onVolumeChange(start);

        const animate = (currentTime) =>
        {
            const elapsed = currentTime - startTime;

            if (elapsed >= msDuration)
            {
                onVolumeChange(target);
                resolve();
            }
            else
            {
                const progress = elapsed / msDuration;
                const currentVol = start + (target - start) * progress;
                onVolumeChange(currentVol);
                requestAnimationFrame(animate);
            }
        };

        requestAnimationFrame(animate);
    });
};
