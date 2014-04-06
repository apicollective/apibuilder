$(function() {

  var submitForm = function(e, url) {
    e.preventDefault();
    $('#openid').val(url);
    $('#openid-form').submit();
  };

  $('#id_google').click(function(e) {
    submitForm(e, 'https://www.google.com/accounts/o8/id');
  });

  $('#id_yahoo').click(function(e) {
    submitForm(e, 'https://me.yahoo.com');
  });

});
