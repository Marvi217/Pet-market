document.addEventListener('click', function(e) {
    const sidebar = document.getElementById('filterSidebar');
    const filterBtn = document.querySelector('.mobile-filter-btn');

    if (sidebar.classList.contains('mobile-open') &&
        !sidebar.contains(e.target) &&
        !filterBtn.contains(e.target)) {
        sidebar.classList.remove('mobile-open');
    }
});

function handleAddToCart(button, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }

    const form = button.closest('.add-to-cart-form');
    const productId = form.querySelector('input[name="productId"]').value;
    const quantity = form.querySelector('input[name="quantity"]').value;

    const params = new URLSearchParams();
    params.append('productId', productId);
    params.append('quantity', quantity);

    button.disabled = true;
    button.textContent = '...';

    fetch('/cart/add-async', {
        method: 'POST',
        body: params,
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    })
        .then(response => {
            if (!response.ok) throw new Error('Błąd');
            return response.json();
        })
        .then(data => {
            const badge = document.querySelector('.cart-badge');
            if (badge) {
                badge.textContent = data.totalItems;
                badge.style.display = 'block';
            }

            button.textContent = 'Dodano! ✓';
            button.style.backgroundColor = '#059669';

            setTimeout(() => {
                button.disabled = false;
                button.textContent = 'Do koszyka';
                button.style.backgroundColor = '';
            }, 2000);
        })
        .catch(error => {
            console.error('Błąd:', error);
            button.disabled = false;
            button.textContent = 'Błąd!';
        });
}


function toggleBrands() {
    const extraBrands = document.querySelectorAll('.extra-brands');
    const btn = document.getElementById('toggleBrandsBtn');

    const isHidden = extraBrands[0].style.display === 'none';
    extraBrands.forEach(el => {
        el.style.display = isHidden ? 'flex' : 'none';});
    btn.textContent = isHidden ? '- Zwiń' : '+ Rozwiń';
}
function handleWishlist(button, event) {
    event.stopPropagation();

    const productId = button.getAttribute('data-product-id');
    const heart = button.querySelector('.heart');
    const isFilled = heart.classList.contains('filled');

    button.style.transform = 'scale(1.2)';
    setTimeout(() => {
        button.style.transform = '';
    }, 200);

    fetch('/wishlist/toggle', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ productId: productId })
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                if (isFilled) {
                    heart.classList.remove('filled');
                    heart.classList.add('empty');
                } else {
                    heart.classList.remove('empty');
                    heart.classList.add('filled');
                }

                showToast(isFilled ? 'Usunięto z ulubionych' : 'Dodano do ulubionych');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('Wystąpił błąd', 'error');
        });
}

function showToast(message, type = 'success') {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'success' ? '#10b981' : '#ef4444'};
        color: white;
        padding: 12px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 9999;
        animation: slideIn 0.3s ease;
    `;
    toast.textContent = message;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 2000);
}

const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
`;
document.head.appendChild(style);