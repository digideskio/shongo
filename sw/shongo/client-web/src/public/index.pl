#!/usr/bin/perl
#
# Shongo web client
#
package main;

use strict;
use warnings;

# Setup lib directory
use FindBin;
use lib "$FindBin::Bin/../main/perl";
use lib "$FindBin::Bin/../../../client-common/src/main/perl";

# Get directory with resources
use File::Spec::Functions;
use File::Basename;
my $resources_directory = File::Spec::Functions::rel2abs(File::Basename::dirname($0)) . '/../resources';

use CGI;
use Template;
use Shongo::Common;
use Shongo::WebClientApplication;
use Shongo::H323SipController;
use Shongo::AdobeConnectController;

# Initialize CGI
my $cgi = CGI->new();

# Initialize templates
my $template = Template->new({
    INCLUDE_PATH  => $resources_directory
});

# Catch errors
use CGI::Carp qw(fatalsToBrowser);
BEGIN {
    sub carp_error {
        my $error = shift;
        my $error_application = Shongo::Web::Application->new($cgi, $template);
        $error_application->error_action($error);
    }
    CGI::Carp::set_die_handler( \&carp_error );
}

# Initialize application
my $application = Shongo::WebClientApplication->new($cgi, $template);
$application->set_controller_url('http://127.0.0.1:8181');
$application->add_action('index', 'index', sub { index_action(); });
$application->add_controller(Shongo::H323SipController->new($application));
$application->add_controller(Shongo::AdobeConnectController->new($application));

#print $cgi->header(type => 'text/html');

# Run application and catch response
my $response = '';
{
    open(CATCHED_OUTPUT, '>', \$response);
    select CATCHED_OUTPUT;

    $application->run($ARGV[0]);

    select STDOUT;
}

# If response doesn't contains headers, add default headers
if ( !($response =~ /^(Status|Content-Type|Location)/) ) {
    print $cgi->header(type => 'text/html');
}
# Print response
print($response);

# Index action
sub index_action
{
    $application->render_page('Shongo', 'index.html');
}
