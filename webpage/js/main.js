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

  // 图片轮播功能
  var activeImg = document.querySelector('.hero-image.active');
  var nextImg = document.querySelector('.hero-image.next');
  if(activeImg && nextImg){
    var images = ['images/intro.png', 'images/intro2.png', 'images/intro3.png', 'images/intro4.png'];
    var currentIndex = 0;
    setInterval(function(){
      // 先更新next的src为下一张
      var nextIndex = (currentIndex + 1) % images.length;
      nextImg.src = images[nextIndex];
      // 然后淡出active，淡入next
      activeImg.style.opacity = '0';
      nextImg.style.opacity = '1';
      // 等待动画完成后交换
      setTimeout(function(){
        activeImg.classList.remove('active');
        activeImg.classList.add('next');
        nextImg.classList.remove('next');
        nextImg.classList.add('active');
        // 更新索引
        currentIndex = nextIndex;
        // 交换引用
        var temp = activeImg;
        activeImg = nextImg;
        nextImg = temp;
      }, 1000);
    }, 5000);
  }
});
