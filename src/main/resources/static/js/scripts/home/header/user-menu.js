export function handleUserMenu(target)
{
    const profileBar = target.querySelector('.user-profile-bar');
    const actionsDropdown = target.querySelector('.user-actions-dropdown');

    if (profileBar && actionsDropdown)
    {
        profileBar.addEventListener('click', (event) =>
        {
            event.stopPropagation();
            profileBar.classList.toggle('active');
            actionsDropdown.classList.toggle('active');
        });

        target.addEventListener('click', (event) =>
        {
            if (!actionsDropdown.contains(event.target) && !profileBar.contains(event.target))
            {
                profileBar.classList.remove('active');
                actionsDropdown.classList.remove('active');
            }
        });
    }
}