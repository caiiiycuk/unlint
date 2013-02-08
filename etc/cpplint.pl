use strict;
use warnings;

use FindBin;
use lib "$FindBin::Bin/lib";
use XMLMessage;

my $file = $ARGV[0];

my @respone = `python etc/cpplint.py --filter=-whitespace/tab,-build/include_order,-legal/copyright,-whitespace/labels,-readability/function,-runtime/rtti $file 2>&1`;

print "<cpplint>\n";

for (@respone) {
    if (/^(.*):(\d+):\s*(.*)$/) {
        my $line = $2;
        my $message = safe($3);
        print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
    }
}

print "</cpplint>\n";

0;
