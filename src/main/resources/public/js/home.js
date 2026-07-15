function toggleUserMenu() {
    const menu = document.getElementById("userDropdownMenu");
    menu.classList.toggle("show");
}

document.addEventListener("click", function (event) {

    const dropdown = document.querySelector(".user-dropdown");
    const menu = document.getElementById("userDropdownMenu");

    if (!dropdown.contains(event.target)) {
        menu.classList.remove("show");
    }

});

const heroImages = [
    "../images/hero-bonsai.png",
    "../images/hero-bonsai-2.png",
    "../images/hero-bonsai-3.png"
];

let currentHeroIndex = 0;

function changeHeroImage() {
    const hero = document.getElementById("heroSlider");

    hero.style.backgroundImage =
        `linear-gradient(rgba(0, 0, 0, 0.25), rgba(0, 0, 0, 0.45)), url('${heroImages[currentHeroIndex]}')`;
}

function nextHeroImage() {
    currentHeroIndex++;

    if (currentHeroIndex >= heroImages.length) {
        currentHeroIndex = 0;
    }

    changeHeroImage();
}

function prevHeroImage() {
    currentHeroIndex--;

    if (currentHeroIndex < 0) {
        currentHeroIndex = heroImages.length - 1;
    }

    changeHeroImage();
}