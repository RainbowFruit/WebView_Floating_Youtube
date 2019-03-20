var player;
   function onYouTubeIframeAPIReady() {
     player = new YT.Player('player', {
                    height: '150',
                       width: '300',
                       events: {
                         'onReady': onPlayerReady,
                         'onStateChange': onPlayerStateChange
                       }
              });
              alert('ready');
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

   function test() {
    alert('im here')
   }