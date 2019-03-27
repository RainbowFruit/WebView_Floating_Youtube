function () {
  document.getElementById("hplogo").src = "https://android.com.pl/images/user-images/2017/03/android-developer2.png";
  document.bgColor = "#454343";
  document.getElementById("lst-ib").value = "Some random text";
}

   var player;
   function onYouTubeIframeAPIReady() {
     player = new YT.Player('player', {
                    height: '150',
                       width: '300',
                       videoId: 'y9jDc7BJIL8',
                       events: {
                         'onReady': onPlayerReady,
                         'onStateChange': onPlayerStateChange
                       }
              });
   }

   function onPlayerReady(event) {
     event.target.playVideo();
   }
   var done = false;

   function onPlayerStateChange(event) {
     if (event.data == YT.PlayerState.PLAYING && !done) {
     //      setTimeout(stopVideo, 6000);
       done = true;
     }
   }

   function stopVideo() {
     player.stopVideo();
   }