document.addEventListener('DOMContentLoaded',function(){
  var navToggle = document.getElementById('nav-toggle');
  var nav = document.getElementById('nav');
  if(navToggle){
    navToggle.addEventListener('click',function(){
      if(nav.style.display==='flex'){nav.style.display='none'}else{nav.style.display='flex'}
    })
  }
  var copyBtn = document.getElementById('copy-build');
  if(copyBtn){
    copyBtn.addEventListener('click',function(){
      var txt = document.getElementById('build-cmd').innerText;
      navigator.clipboard.writeText(txt).then(function(){
        copyBtn.innerText='已复制';
        setTimeout(()=>copyBtn.innerText='复制命令',1500);
      });
    })
  }

  // 轮播逻辑
  var currentImage = document.querySelector('.carousel-image.current');
  var nextImage = document.querySelector('.carousel-image.next');
  var images = ['images/intro.png', 'images/intro2.png', 'images/intro3.png', 'images/intro4.png'];
  var currentIndex = 0;

  function nextSlide() {
    var nextIndex = (currentIndex + 1) % images.length;
    nextImage.src = images[nextIndex];
    nextImage.style.opacity = '1';
    setTimeout(function() {
      currentImage.src = images[nextIndex];
      nextImage.style.opacity = '0';
      currentIndex = nextIndex;
    }, 1000); // 过渡时间1秒
  }

  setInterval(nextSlide, 5000); // 每5秒切换
});
