use strict;
use warnings;

my $file = $ARGV[0];

my @respone = `python etc/cpplint.py --filter=-whitespace/tab,-build/include_order,-legal/copyright $file 2>&1`;

print "<cpplint>\n";

for (@respone) {
    if (/^(.*):(\d+):\s*(.*)$/) {
        my $line = $2;
        my $message = $3;

        $message =~ s/"/&quot;/g;
        $message =~ s/</&lt;/g;
        $message =~ s/>/&gt;/g;
        $message =~ s{/}{|}g;

        print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
    }
}

print "</cpplint>\n";

0;