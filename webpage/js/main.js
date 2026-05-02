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
});
