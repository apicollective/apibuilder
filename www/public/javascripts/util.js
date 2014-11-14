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

  $('.generator-enable-setter').on('click', function() {
    var guid = $(this).data('generator-guid'),
        enabled = $(this).is(':checked');
    $.ajax({
      type: "POST",
      url: "/generators/postUpdate?generatorGuid=" + guid,
      data: { "generatorGuid": guid, "enabled": enabled }
    })
  });

  $('.generator-visibility-setter').on('change', function() {
    var guid = $(this).data('generator-guid'),
        visibility = $(this).val();
    $.ajax({
      type: "POST",
      url: "/generators/postUpdate?generatorGuid=" + guid,
      data: { "generatorGuid": guid, "visibility": visibility }
    })
  });


  $('.generator-select').on('click', function() {
    var guid = $(this).data('generator-guid');
    location.href = "/generators/" + guid;
  });
});
