(function(window, $) {
    $(function() {
        function success(position) {
            var latitude = position.coords.latitude
            var longitude = position.coords.longitude

            output.html(latitude + '°N, ' + longitude + '°E')

            send({
                location : latitude + ',' + longitude
            });
        }

        function error() {
            $('.geolocation').hide()
        }

        $('.geolocation').hide()

        if (!navigator.geolocation || !navigator.geolocation.getCurrentPosition) {
            return

        }

        var output = $('#geolocation')

        $.getJSON(window.location.origin + '/rest/services/org.eclipse.smarthome.core.i18nprovider/config', function(response) {
            window.language = response;

            if (!response.location) {
                $('.geolocation').show()
                output.html("<div class=\"spinner spinner--steps\"><img src=\"img/spinner.svg\"></div>")
                navigator.geolocation.getCurrentPosition(success, error)
            }
        })
    })

    function send(configuration) {
        $.ajax({
            url : window.location.origin + '/rest/services/org.eclipse.smarthome.core.i18nprovider/config',
            data : JSON.stringify(configuration),
            type : 'PUT',
            dataType : 'json',
            contentType : 'application/json'
        }).done(function(response) {
            console.log('configuration send: ' + response);
        }).fail(function(xhr, status, errorThrown) {
            console.log('Request failed. Returned status of ' + status)
        })
    }
})(window, $)