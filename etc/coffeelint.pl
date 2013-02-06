use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/lib";
use XMLMessage;

my $file = $ARGV[0];

my @respone = `coffeelint --csv $file`;

print "<coffeelint>\n";

for my $line (@respone) {
    my @cells = split ',', $line;
    my $line = $cells[1];
    my $message = safe($cells[3]);

    print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
}

print "</coffeelint>\n";

0;