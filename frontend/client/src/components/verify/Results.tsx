import { motion } from "framer-motion";
import { type VerificationResultResponse } from "@shared/routes";
import { Card } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { AlertTriangle, CheckCircle, Info, ShieldAlert, ScanFace, FileText, Calendar, Fingerprint } from "lucide-react";
import { clsx } from "clsx";

interface ResultsProps {
  result: VerificationResultResponse;
}

export function VerifyResults({ result }: ResultsProps) {
  // Determine colors based on risk level
  const isHighRisk = result.riskScore >= 75 || result.riskLevel.toLowerCase() === 'high';
  const isLowRisk = result.riskScore <= 30 || result.riskLevel.toLowerCase() === 'low';
  
  const statusColorClass = isHighRisk 
    ? "text-destructive border-destructive/30 bg-destructive/10" 
    : isLowRisk 
      ? "text-success border-success/30 bg-success/10" 
      : "text-warning border-warning/30 bg-warning/10";

  const progressColorClass = isHighRisk ? "bg-destructive" : isLowRisk ? "bg-success" : "bg-warning";
  const glowClass = isHighRisk ? "cyber-glow-destructive" : isLowRisk ? "cyber-glow-success" : "cyber-glow";

  const containerVariants = {
    hidden: { opacity: 0 },
    show: {
      opacity: 1,
      transition: { staggerChildren: 0.1 }
    }
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0, transition: { type: "spring", stiffness: 300, damping: 24 } }
  };

  return (
    <motion.div 
      variants={containerVariants}
      initial="hidden"
      animate="show"
      className="w-full max-w-4xl mx-auto mt-8 grid grid-cols-1 md:grid-cols-12 gap-6"
    >
      {/* Primary Status Card */}
      <motion.div variants={itemVariants} className="md:col-span-12">
        <Card className={clsx("p-6 glass-panel border overflow-hidden relative", glowClass)}>
          <div className="absolute top-0 left-0 w-1 h-full bg-current opacity-50" style={{ color: isHighRisk ? 'hsl(var(--destructive))' : isLowRisk ? 'hsl(var(--success))' : 'hsl(var(--warning))' }} />
          
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
            <div className="flex items-center gap-4">
              <div className={clsx("w-16 h-16 rounded-full flex items-center justify-center border", statusColorClass)}>
                {isHighRisk ? <ShieldAlert className="w-8 h-8" /> : isLowRisk ? <CheckCircle className="w-8 h-8" /> : <AlertTriangle className="w-8 h-8" />}
              </div>
              <div>
                <p className="text-sm font-mono text-muted-foreground uppercase tracking-widest mb-1">Status</p>
                <h2 className="text-3xl font-display font-bold text-foreground capitalize">
                  {result.riskLevel} Risk
                </h2>
              </div>
            </div>

            <div className="w-full md:w-1/3 space-y-2">
              <div className="flex justify-between items-end">
                <span className="text-sm font-mono text-muted-foreground">Confidence Score</span>
                <span className="font-mono font-bold text-lg">{result.riskScore}%</span>
              </div>
              <div className="h-3 w-full bg-secondary rounded-full overflow-hidden border border-border">
                <motion.div 
                  initial={{ width: 0 }}
                  animate={{ width: `${result.riskScore}%` }}
                  transition={{ duration: 1, ease: "easeOut", delay: 0.5 }}
                  className={clsx("h-full", progressColorClass)}
                />
              </div>
            </div>
          </div>
        </Card>
      </motion.div>

      {/* Extracted Data Card */}
      <motion.div variants={itemVariants} className="md:col-span-7">
        <Card className="p-6 glass-panel h-full cyber-border">
          <div className="flex items-center gap-2 mb-6 border-b border-border/50 pb-4">
            <ScanFace className="w-5 h-5 text-primary" />
            <h3 className="font-display font-semibold text-lg">Extracted Entities</h3>
          </div>
          
          <div className="space-y-4">
            <div className="grid grid-cols-3 gap-4 p-3 rounded-lg bg-background/50 border border-border/50 hover:bg-secondary/30 transition-colors">
              <div className="col-span-1 flex items-center gap-2 text-muted-foreground">
                <FileText className="w-4 h-4" />
                <span className="text-sm font-mono">Full Name</span>
              </div>
              <div className="col-span-2 font-medium text-foreground">
                {result.extractedData.name || "UNREADABLE"}
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4 p-3 rounded-lg bg-background/50 border border-border/50 hover:bg-secondary/30 transition-colors">
              <div className="col-span-1 flex items-center gap-2 text-muted-foreground">
                <Fingerprint className="w-4 h-4" />
                <span className="text-sm font-mono">Document ID</span>
              </div>
              <div className="col-span-2 font-mono text-primary">
                {result.extractedData.idNumber || "UNREADABLE"}
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4 p-3 rounded-lg bg-background/50 border border-border/50 hover:bg-secondary/30 transition-colors">
              <div className="col-span-1 flex items-center gap-2 text-muted-foreground">
                <Calendar className="w-4 h-4" />
                <span className="text-sm font-mono">Date of Birth</span>
              </div>
              <div className="col-span-2 font-medium text-foreground">
                {result.extractedData.dob || "UNREADABLE"}
              </div>
            </div>
          </div>
        </Card>
      </motion.div>

      {/* Analysis Engine Log */}
      <motion.div variants={itemVariants} className="md:col-span-5">
        <Card className="p-0 glass-panel h-full cyber-border overflow-hidden flex flex-col">
          <div className="p-4 border-b border-border/50 bg-secondary/20 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Info className="w-4 h-4 text-primary" />
              <h3 className="font-display font-semibold text-sm">AI Analysis Log</h3>
            </div>
            <Badge variant="outline" className="font-mono text-[10px] bg-background/50">SYSTEM_v4.2</Badge>
          </div>
          
          <div className="p-4 flex-1 bg-background/30 font-mono text-sm space-y-3 overflow-y-auto">
            {result.explanation.map((exp, idx) => (
              <motion.div 
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.8 + (idx * 0.2) }}
                key={idx} 
                className="flex items-start gap-2 text-muted-foreground"
              >
                <span className="text-primary mt-0.5">&gt;</span>
                <span className={clsx(
                  exp.toLowerCase().includes('fail') || exp.toLowerCase().includes('detect') || exp.toLowerCase().includes('mismatch') 
                    ? "text-destructive" 
                    : exp.toLowerCase().includes('pass') || exp.toLowerCase().includes('match') 
                      ? "text-success" 
                      : "text-foreground"
                )}>
                  {exp}
                </span>
              </motion.div>
            ))}
          </div>
        </Card>
      </motion.div>
    </motion.div>
  );
}
