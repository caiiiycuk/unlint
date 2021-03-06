package XMLMessage;

use Exporter 'import';
@EXPORT = qw(safe);

sub safe {
    my $message = shift;
    $message =~ s/"/&quot;/g;
    $message =~ s/</&lt;/g;
    $message =~ s/>/&gt;/g;
    $message =~ s/&/&amp;/g;
    return $message;
}

1;