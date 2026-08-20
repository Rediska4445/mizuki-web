const fileInput = document.getElementById('fileInput');
const fileLabel = document.querySelector('.file-label');

fileInput.addEventListener('change', function() {
    if (this.files.length > 0) {
        const fileName = this.files[0].name;
        fileLabel.textContent = `Выбран: ${fileName}`;
        fileLabel.classList.add('has-file');
    } else {
        fileLabel.textContent = 'Choose audio file...';
        fileLabel.classList.remove('has-file');
    }
});