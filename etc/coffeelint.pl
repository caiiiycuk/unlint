use strict;
use warnings;

my $file = $ARGV[0];

my @respone = `coffeelint --csv $file`;

print "<coffeelint>\n";

for my $line (@respone) {
    my @cells = split ',', $line;
    my $line = $cells[1];
    my $message = $cells[3];

    $message =~ s/"/&quot;/g;
    $message =~ s/</&lt;/g;
    $message =~ s/>/&gt;/g;
    $message =~ s{/}{|}g;

    print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
}

print "</coffeelint>\n";

0;