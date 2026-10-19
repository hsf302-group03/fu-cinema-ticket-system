(() => {
    const form = document.getElementById('booking-form');
    const seats = [...form.querySelectorAll('input[name="seatCodes"]:not(:disabled)')];
    const button = document.getElementById('confirm-booking');
    const help = document.getElementById('seat-help');
    const money = new Intl.NumberFormat('vi-VN');
    const update = () => {
        const selected = seats.filter(seat => seat.checked);
        document.getElementById('selected-seats').textContent = selected.map(seat => seat.value).join(', ') || 'Chưa chọn ghế';
        document.getElementById('booking-total').textContent = money.format(selected.length * Number(form.dataset.price)) + ' đ';
        button.disabled = selected.length === 0;
    };
    seats.forEach(seat => seat.addEventListener('change', () => {
        if (seats.filter(item => item.checked).length > 8) {
            seat.checked = false;
            help.textContent = 'Bạn đã chọn 8 ghế. Bỏ chọn một ghế trước khi chọn ghế khác.';
        } else {
            help.textContent = 'Mỗi đơn tối đa 8 ghế. Giá cuối cùng được xác nhận khi đặt vé.';
        }
        update();
    }));
    form.addEventListener('submit', () => {
        button.disabled = true;
        button.textContent = 'Đang đặt vé…';
    });
    window.addEventListener('pageshow', () => {
        button.textContent = 'Xác nhận đặt vé';
        update();
    });
    update();
})();
