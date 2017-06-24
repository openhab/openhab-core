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
            output.html("Unable to retrieve your location.")
        }

        $('.geolocation').hide()

        if (!navigator.geolocation || !navigator.geolocation.getCurrentPosition) {
            return

        }

        var output = $('#geolocation')

        $.getJSON('../rest/services/org.eclipse.smarthome.core.i18nprovider/config', function(response) {
            if (!response.location) {
                $('.geolocation').show()
                output.html("Retrieving your location…")
                navigator.geolocation.getCurrentPosition(success, error)
            }
        })
    })

    function send(configuration) {
        $.ajax({
            url : '../rest/services/org.eclipse.smarthome.core.i18nprovider/config',
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