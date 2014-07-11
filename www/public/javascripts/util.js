$(function(){
  $('.delete').each(function(i, el) {
    el.addEventListener('click', function(event) {
      event.preventDefault();
      var text = this.getAttribute('data-confirm')
      var href = this.getAttribute('href');
      if (confirm(text)) {
        $('<form method="post" action="' + href + '"></form>').appendTo('body').submit();
      }
    });
  });

  $('a.postForm').each(function(i, el) {
    el.addEventListener('click', function(event) {
      event.preventDefault();
      var href = this.getAttribute('href');
      $('<form method="post" action="' + href + '"></form>').appendTo('body').submit();
    });
  });
});
