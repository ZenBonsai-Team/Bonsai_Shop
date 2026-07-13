/* ==========================================================================
   Bonsai Luxury — Interactive Premium Script
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {

    // Rút gọn phương thức Selector tuyển chọn mẫu
    const $ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));
    const one = (sel, ctx = document) => ctx.querySelector(sel);
    const escapeHtml = s => String(s).replace(/[&<>"']/g, m => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[m]);

    // Toast thông báo chuẩn cao cấp
    const showToast = (text) => {
        const existing = one('.toast');
        if (existing) existing.remove();

        const t = document.createElement('div');
        t.className = 'toast';
        t.textContent = text;
        document.body.appendChild(t);
        setTimeout(() => {
            t.style.opacity = '0';
            t.style.transition = 'opacity 0.5s ease';
            setTimeout(() => t.remove(), 500);
        }, 4000);
    };

    /* ----------------------------------------------------------------------
       1. THUMBNAIL INTERACTION (Chuyển đổi ảnh chính mượt mà)
       ---------------------------------------------------------------------- */
    const mainImg = one('#mainImage');
    const thumbBtns = $('.thumb');

    thumbBtns.forEach(thumb => {
        thumb.addEventListener('click', function() {
            if (!mainImg) return;

            // Xóa trạng thái active cũ, gán trạng thái mới
            thumbBtns.forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            // Lấy link ảnh từ thuộc tính data-src của Thymeleaf attribute gán sang
            const newSrc = this.getAttribute('data-src');
            if (newSrc) {
                mainImg.style.opacity = '0.3';
                setTimeout(() => {
                    mainImg.src = newSrc;
                    mainImg.style.opacity = '1';
                }, 15000); // Đổi ảnh mượt trong 150ms trễ
                mainImg.style.opacity = '1';
                mainImg.src = newSrc;
            }
        });
    });

    /* ----------------------------------------------------------------------
       2. LIGHTBOX PREVIEW GALLERY (Phóng to ảnh chi tiết lúc click)
       ---------------------------------------------------------------------- */
    const galleryItems = $('.gallery-item');
    const overlayRoot = document.getElementById('overlayRoot') || (() => {
        const d = document.createElement('div'); d.id = 'overlayRoot'; document.body.appendChild(d); return d;
    })();

    galleryItems.forEach(item => {
        item.addEventListener('click', function() {
            const imgSrc = this.getAttribute('data-src') || one('img', this)?.src;
            if (!imgSrc) return;

            const lightbox = document.createElement('div');
            lightbox.className = 'popup';
            lightbox.innerHTML = `
                <div class="popup-content" style="max-width: 800px; padding: 10px; background: transparent; border: none; box-shadow: none;">
                    <button class="popup-close" style="color: #FFF; background: rgba(0,0,0,0.5); top: -40px; right: 0;">✕</button>
                    <img src="${escapeHtml(imgSrc)}" alt="Bonsai Luxury Zoom" style="width: 100%; height: auto; max-height: 85vh; object-fit: contain; border: 1px solid rgba(255,255,255,0.2);">
                </div>
            `;
            overlayRoot.appendChild(lightbox);

            const closeBox = () => lightbox.remove();
            lightbox.querySelector('.popup-close')?.addEventListener('click', closeBox);
            lightbox.addEventListener('click', (ev) => { if (ev.target === lightbox) closeBox(); });
        });
    });

    /* ----------------------------------------------------------------------
    3. PREMIUM BOOKING MODAL (Thymeleaf Form + Login Check)
    ---------------------------------------------------------------------- */

    const bookingModal = document.getElementById('bookingModal');

    const modalProductId = document.getElementById('modalProductId');
    const modalProductTitle = document.getElementById('modalProductTitle');

    const actualBookingForm = document.getElementById('actualBookingForm');

    const closeBookingBtn = document.getElementById('closeBookingBtn');
    const cancelBookingBtn = document.getElementById('cancelBookingBtn');

    const dateInput = document.getElementById('appointmentDate');
    const timeInput = document.getElementById('appointmentTime');


// ===============================
// ĐÓNG MODAL
// ===============================
    const closeModal = () => {

        if(!bookingModal) return;

        bookingModal.style.opacity = '0';

        setTimeout(()=>{
            bookingModal.style.display = 'none';
            bookingModal.setAttribute(
                'aria-hidden',
                'true'
            );

        },300);
    };


// ===============================
// MỞ MODAL
// ===============================
    const openBookingModal = (e)=>{

        const btn = e.currentTarget;

        const id = btn.dataset.id;


        // lấy tên sản phẩm
        const title =
            document.getElementById('productTitle')
                ?.textContent
                ?.trim()
            ||
            "Tác phẩm độc bản";


        if(modalProductId){
            modalProductId.value = id;
        }


        if(modalProductTitle){
            modalProductTitle.textContent = title;
        }



        // ngày mặc định ngày mai

        if(dateInput){

            const tomorrow = new Date();

            tomorrow.setDate(
                tomorrow.getDate()+1
            );


            const yyyy =
                tomorrow.getFullYear();

            const mm =
                String(
                    tomorrow.getMonth()+1
                ).padStart(2,'0');


            const dd =
                String(
                    tomorrow.getDate()
                ).padStart(2,'0');


            const minDate =
                `${yyyy}-${mm}-${dd}`;


            dateInput.min = minDate;

            dateInput.value = minDate;
        }



        if(timeInput){
            timeInput.value="";
        }



        // hiện modal

        if(bookingModal){

            bookingModal.style.display='flex';

            bookingModal.setAttribute(
                'aria-hidden',
                'false'
            );


            void bookingModal.offsetWidth;


            bookingModal.style.opacity='1';

        }

    };



// ===============================
// CLICK ĐẶT LỊCH
// ===============================

    document
        .querySelectorAll('.schedule-btn')
        .forEach(btn=>{


            btn.addEventListener(
                'click',
                (e)=>{


                    /*
                      Kiểm tra login
                      Nếu chưa login -> login page
                    */

                    const authenticated =
                        document.body.dataset.authenticated === "true";


                    if(!authenticated){

                        window.location.href="/login";

                        return;
                    }



                    openBookingModal(e);

                }
            );


        });



// ===============================
// BUTTON CLOSE
// ===============================

    closeBookingBtn
        ?.addEventListener(
            'click',
            closeModal
        );


    cancelBookingBtn
        ?.addEventListener(
            'click',
            closeModal
        );



// click ra ngoài

    bookingModal
        ?.addEventListener(
            'click',
            e=>{

                if(e.target === bookingModal){

                    closeModal();

                }

            }
        );



// ===============================
// VALIDATE FORM
// ===============================

    actualBookingForm
        ?.addEventListener(
            'submit',
            e=>{


                // check ngày

                if(dateInput.value){

                    const selected =
                        new Date(dateInput.value);


                    const today =
                        new Date();


                    today.setHours(
                        0,0,0,0
                    );


                    selected.setHours(
                        0,0,0,0
                    );



                    if(selected <= today){

                        e.preventDefault();

                        showToast(
                            "Vui lòng chọn ngày từ ngày mai."
                        );

                        return;
                    }

                }



                // check giờ

                if(!timeInput.value){

                    e.preventDefault();

                    showToast(
                        "Vui lòng chọn giờ xem."
                    );

                    return;

                }



                if(
                    timeInput.value < "08:00"
                    ||
                    timeInput.value > "17:00"
                ){

                    e.preventDefault();


                    showToast(
                        "Vui lòng chọn thời gian từ 08:00 đến 17:00."
                    );


                    return;

                }



                const submitBtn =
                    actualBookingForm
                        .querySelector(
                            ".submit-booking-btn"
                        );


                if(submitBtn){

                    submitBtn.disabled=true;

                    submitBtn.textContent=
                        "Đang xử lý...";

                }


            }
        );
});