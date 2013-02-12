use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/lib";
use XMLMessage;

my $file = $ARGV[0];

my @respone = `coffeelint -f etc/coffeelint.json --nocolor $file`;

print "<coffeelint>\n";

for my $line (@respone) {
    if ($line =~ m/#(\d+):(.*)$/) {
        my $line = $1;
        my $message = safe($2);
        print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
    }
}

print "</coffeelint>\n";

0;