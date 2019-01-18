var LANDING_PAGE_HERO_HEIGHT = 550 - 64;

$(function() {
	window.isSmall = $(window).width() <= 600;
    window.pathArray = window.location.pathname.split( '/' );
    window.onLandingPage = ($.inArray('index.html', window.pathArray) >= 0 
    		&& $.inArray('documentation', window.pathArray) < 0) 
    		|| window.pathArray[window.pathArray.length -1].length == 0;
        
    initEffects();
    initSideNav();
    initStickyHeader();
});

function initEffects() {
    // Pipes Slide & Custom Parallax
    $('#hero .img-wrapper').slideDown();
   	function parallaxScroll(){
   		var scrolledY = $(window).scrollTop();
		$('#hero .img-wrapper').css('bottom','-'+((scrolledY*0.1))+'px');
   	}
   	
   	if(!window.isSmall) {
	    $(window).bind('scroll',function(e){
	        parallaxScroll();
	    });
   	}
    
    // Slide In Top Level Menu
    $(".button-collapse").sideNav();
    
    // Generel Parallax Effect 
    $('.parallax').parallax();
    
  
    // Code Syntax Highlighting
    hljs.initHighlightingOnLoad();
    
    // Striped Tables
    $('section#documentation table').addClass('striped');
    
    // Linkable Headers
    $('body.documentation section#documentation h2, body.documentation section#documentation h3').click(function(event) {
		var id = $(event.target).attr('id');
		window.location.hash = id;
	});
}

function initStickyHeader() {
    var stickyHeaderPosition = !window.onLandingPage ? 1 : LANDING_PAGE_HERO_HEIGHT;
    
    $(window).scroll(function() {
        if ($(this).scrollTop() > stickyHeaderPosition) {
            $('#header').addClass("sticky");
            $('#header').addClass("z-depth-1");
        } else {
            $('#header').removeClass("sticky");
            $('#header').removeClass("z-depth-1");
        }
    }); 
}

function initSideNav() {
    var linkUrl = getLinkUrl();
    $('section ul.nav a').each(function(index, element) {
        if($(element).attr('href').indexOf(linkUrl) != -1) {
            $(element).closest('li').addClass('active');
            $(element).closest('li').parents('li').addClass('active open');
            $(element).parents('ul').slideDown();
        }
        if($(element).closest('li').children('ul').length > 0) {
            $(element).addClass('has-submenu');
            // Accordion
            /*$(element).click(function(event) {
                event.stopPropagation();
                $('section#documentation ul.nav ul').slideUp();
                $(element).closest('li').children('ul').slideDown();
                return false;
            });*/
        }
    });

    // Sticky SideNav
    if(!window.onLandingPage) {
        var isSmall = $(window).width() <= 600;
        
        if(!isSmall) {
            // $('section ul.nav').sticky({topSpacing: 100});
        }
        
        $( window ).resize(function() {
            var isSmall = $(window).width() <= 600;
            if(isSmall) {
                // $('section ul.nav').unstick();
            } else {
                // $('section ul.nav').sticky({topSpacing: 100});
            }
        });
    }
}

function getLinkUrl() {
    var indexOfDocumentation = window.pathArray.indexOf('documentation');
    var linkUrl = 'documentation/';
    for (i = indexOfDocumentation + 1; i < pathArray.length; i++) { 
        var pathSegment = pathArray[i];
        if(pathSegment.indexOf('.html') != -1) {
            linkUrl += pathSegment;   
        } else {
            linkUrl += pathSegment + "/";   
        }
    }
    return linkUrl;
}