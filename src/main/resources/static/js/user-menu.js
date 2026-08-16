document.addEventListener('DOMContentLoaded', () => {
    const profileBar = document.querySelector('.user-profile-bar');
    const actionsDropdown = document.querySelector('.user-actions-dropdown');

    if (profileBar && actionsDropdown) {
        profileBar.addEventListener('click', (event) => {
            event.stopPropagation();
            profileBar.classList.toggle('active');
            actionsDropdown.classList.toggle('active');
        });

        document.addEventListener('click', (event) => {
            if (!actionsDropdown.contains(event.target) && !profileBar.contains(event.target)) {
                profileBar.classList.remove('active');
                actionsDropdown.classList.remove('active');
            }
        });

        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                profileBar.classList.remove('active');
                actionsDropdown.classList.remove('active');
            }
        });
    }
});
