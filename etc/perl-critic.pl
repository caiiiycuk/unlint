use FindBin;
use lib "$FindBin::Bin/lib";

use Perl::Critic qw(critique);
use XMLMessage;

@violations = critique($ARGV[0]);

print '<perl-critic>', "\n";
for (@violations) {
	my $line = $_->line_number();
	my $message = safe( $_->description() . ": " . $_->explanation() );
    print "<error line=\"$line\" severity=\"warning\" message=\"$message\"></error>\n";
}
print '</perl-critic>', "\n";