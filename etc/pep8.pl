use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/lib";
use XMLMessage;

my $file = $ARGV[0];

my @respone = `pep8 $file`;

print "<pep8>\n";

for (@respone) {
	#etc/cpplint.py:3805:3: E111 indentation is not a multiple of four
    if (/^.*:(\d+):(\d+):\s*(.*)$/) {
        my $line = $1;
        my $message = safe($3);
        print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
    }
}

print "</pep8>\n";

0;
