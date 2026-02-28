import { Navbar } from "@/components/layout/Navbar";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from "@/components/ui/table";
import { Activity, ShieldAlert, CheckCircle, Search, Filter, RefreshCw } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useEffect, useState } from "react";
import { API_ENDPOINTS } from "@/config/api";

interface VerificationRecord {
  id: string;
  fileName: string;
  createdAt: string;
  riskLevel: string;
  riskScore: number;
  extractedData: {
    name: string;
    idNumber: string;
    dob: string;
  };
}

interface StatsData {
  totalVerifications: number;
  lowRiskCount: number;
  mediumRiskCount: number;
  highRiskCount: number;
  averageRiskScore: number;
  riskDistribution: {
    low: number;
    medium: number;
    high: number;
  };
}

export default function AdminDashboard() {
  const [records, setRecords] = useState<VerificationRecord[]>([]);
  const [stats, setStats] = useState<StatsData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = async () => {
    try {
      const response = await fetch(API_ENDPOINTS.STATS);
      if (response.ok) {
        const data = await response.json();
        setStats(data);
      }
    } catch (err) {
      console.error('Failed to fetch stats:', err);
    }
  };

  const fetchVerifications = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_ENDPOINTS.VERIFICATIONS}?limit=50`);
      if (response.ok) {
        const data = await response.json();
        setRecords(data);
        setError(null);
      } else {
        setError('Failed to load verification records');
      }
    } catch (err) {
      setError('Failed to connect to backend service');
      console.error('Failed to fetch verifications:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
    fetchVerifications();
  }, []);

  const handleRefresh = () => {
    fetchStats();
    fetchVerifications();
  };

  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      
      <main className="flex-1 container mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-display font-bold">Command Center</h1>
            <p className="text-muted-foreground mt-1">Real-time overview of verification activities</p>
          </div>
          <div className="flex items-center gap-4">
            <Button 
              variant="outline" 
              size="sm" 
              onClick={handleRefresh}
              disabled={loading}
              className="border-border/50"
            >
              <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
            <div className="hidden md:flex items-center gap-2 bg-primary/10 text-primary px-3 py-1.5 rounded-full border border-primary/20 text-sm font-mono">
              <Activity className="w-4 h-4 animate-pulse" />
              SYSTEM_ONLINE
            </div>
          </div>
        </div>

        {/* Top metrics */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <Card className="p-6 glass-panel cyber-border">
            <h3 className="text-sm font-mono text-muted-foreground mb-2">Total Verifications</h3>
            <p className="text-4xl font-display font-bold">
              {stats ? stats.totalVerifications : '0'}
            </p>
            <p className="text-xs text-muted-foreground mt-2">
              All time verifications processed
            </p>
          </Card>
          <Card className="p-6 glass-panel border-success/30 bg-success/5">
            <h3 className="text-sm font-mono text-success/80 mb-2">Low Risk</h3>
            <p className="text-4xl font-display font-bold text-success">
              {stats ? stats.lowRiskCount : '0'}
            </p>
            <p className="text-xs text-muted-foreground mt-2">
              {stats ? `${Math.round((stats.lowRiskCount / Math.max(stats.totalVerifications, 1)) * 100)}% clear rate` : '0% clear rate'}
            </p>
          </Card>
          <Card className="p-6 glass-panel border-destructive/30 bg-destructive/5 cyber-glow-destructive">
            <h3 className="text-sm font-mono text-destructive/80 mb-2">High Risk</h3>
            <p className="text-4xl font-display font-bold text-destructive">
              {stats ? stats.highRiskCount : '0'}
            </p>
            <p className="text-xs text-muted-foreground mt-2">
              Requires manual review
            </p>
          </Card>
        </div>

        {/* Recent Scans Table */}
        <Card className="glass-panel cyber-border overflow-hidden">
          <div className="p-4 border-b border-border/50 flex flex-col md:flex-row gap-4 justify-between items-center bg-secondary/10">
            <h2 className="font-display font-semibold text-lg">Recent Verifications</h2>
            <div className="flex w-full md:w-auto items-center gap-2">
              <div className="relative w-full md:w-64">
                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input 
                  placeholder="Search ID or Name..." 
                  className="pl-9 bg-background/50 border-border/50 font-mono text-sm h-9"
                />
              </div>
              <Button variant="outline" size="icon" className="h-9 w-9 border-border/50">
                <Filter className="h-4 w-4 text-muted-foreground" />
              </Button>
            </div>
          </div>
          
          <div className="overflow-x-auto">
            {loading ? (
              <div className="p-8 text-center text-muted-foreground">
                <Activity className="w-8 h-8 animate-spin mx-auto mb-2" />
                <p>Loading verification records...</p>
              </div>
            ) : error ? (
              <div className="p-8 text-center text-destructive">
                <p>{error}</p>
                <Button variant="outline" size="sm" className="mt-2" onClick={handleRefresh}>
                  Retry
                </Button>
              </div>
            ) : records.length === 0 ? (
              <div className="p-8 text-center text-muted-foreground">
                <p>No verification records found</p>
              </div>
            ) : (
              <Table>
                <TableHeader className="bg-background/40 hover:bg-background/40">
                  <TableRow className="border-border/50">
                    <TableHead className="font-mono text-xs text-muted-foreground">VERIFICATION ID</TableHead>
                    <TableHead className="font-mono text-xs text-muted-foreground">TIMESTAMP</TableHead>
                    <TableHead className="font-mono text-xs text-muted-foreground">EXTRACTED NAME</TableHead>
                    <TableHead className="font-mono text-xs text-muted-foreground">FILE NAME</TableHead>
                    <TableHead className="font-mono text-xs text-muted-foreground">RISK LEVEL</TableHead>
                    <TableHead className="font-mono text-xs text-muted-foreground text-right">SCORE</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {records.map((record) => (
                    <TableRow key={record.id} className="border-border/50 hover:bg-secondary/20 transition-colors">
                      <TableCell className="font-mono text-xs text-primary">{record.id.substring(0, 12)}</TableCell>
                      <TableCell className="font-mono text-xs text-muted-foreground">
                        {new Date(record.createdAt).toLocaleString()}
                      </TableCell>
                      <TableCell className="font-medium">
                        {record.extractedData?.name || 'NOT EXTRACTED'}
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm truncate max-w-[150px]">
                        {record.fileName}
                      </TableCell>
                      <TableCell>
                        {record.riskLevel === 'HIGH RISK' ? (
                          <Badge variant="outline" className="bg-destructive/10 text-destructive border-destructive/20 gap-1">
                            <ShieldAlert className="w-3 h-3" /> High ({record.riskScore})
                          </Badge>
                        ) : record.riskLevel === 'MEDIUM RISK' ? (
                          <Badge variant="outline" className="bg-warning/10 text-warning border-warning/20 gap-1">
                            <Activity className="w-3 h-3" /> Medium ({record.riskScore})
                          </Badge>
                        ) : (
                          <Badge variant="outline" className="bg-success/10 text-success border-success/20 gap-1">
                            <CheckCircle className="w-3 h-3" /> Low ({record.riskScore})
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right font-mono text-sm font-medium">
                        {record.riskScore}%
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </Card>
      </main>
    </div>
  );
}
